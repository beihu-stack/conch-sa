package com.nabob.conch.sa.core.v8;

import org.apache.commons.lang3.StringUtils;
import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.Method;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.utilities.MethodArray;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Jvm Method Counter
 *
 * @author Adam
 * @since 2023/12/18
 */
public class JvmMethodCounter {

    /**
     * 写日志 Writer
     */
    private volatile PrintWriter writer = null;

    public static void main(String[] args) {
        JvmMethodCounter jvmMethodCounter = new JvmMethodCounter();

        /*
            Test-Result CSV:
            className,methodName,signature,invocationCount
            com/nabob/conch/core/support/demo/TargetProcess,main,([Ljava/lang/String;)V,1
            com/nabob/conch/core/support/demo/TargetProcess$Student,setId,(I)V,1
            com/nabob/conch/core/support/demo/TargetProcess$Student,JITTest2,()V,270
            com/nabob/conch/core/support/demo/TargetProcess$Student,JITTest1,()V,100
            com/nabob/conch/core/support/demo/TargetProcess$Student,JITTest3,()V,268
         */
//        jvmMethodCounter.handle(1552, 10000, "com.nabob", "com.dao", "conch_sa_v8", 100);

        /*
            Test-Result-Async CSV:
            className,methodName,signature,invocationCount
            com/nabob/conch/core/support/demo/TargetProcess$Student,JITTest1,()V,100
            com/nabob/conch/core/support/demo/TargetProcess$Student,setId,(I)V,1
            com/nabob/conch/core/support/demo/TargetProcess$Student,JITTest4,(Ljava/lang/String;)V,0
            com/nabob/conch/core/support/demo/TargetProcess$Student,JITTest2,()V,267
            com/nabob/conch/core/support/demo/TargetProcess$Student,JITTest3,()V,268
            com/nabob/conch/core/support/demo/TargetProcess,main,([Ljava/lang/String;)V,1
         */
        jvmMethodCounter.handleAsync(3148, 10000, "com.nabob", "com.dao", "conch_sa_v8", 100);

    }

    private void handle(int pid, long timeout, String filterPrefix, String excludePrefix, String directory, Integer bufferSize) {
        filterPrefix = filterPrefix.replace(".", "/");
        Config config = new Config(pid, timeout, filterPrefix, excludePrefix, directory, bufferSize);

        deleteOldFiles(config);

        openWriter(config);

        HotSpotAgent agent = new HotSpotAgent();
        agent.attach(pid);

        try {
            VM.getVM().getSystemDictionary().allClassesDo(new InvocationCounterVisitor(config));
        } catch (Exception e) {
            reportError(null, e);
        } finally {
            agent.detach();
        }
        closeWriter();
    }

    private void handleAsync(int pid, long timeout, String filterPrefix, String excludePrefix, String directory, Integer bufferSize) {
        filterPrefix = filterPrefix.replace(".", "/");
        Config config = new Config(pid, timeout, filterPrefix, excludePrefix, directory, bufferSize);

        deleteOldFiles(config);

        openWriter(config);

        HotSpotAgent agent = new HotSpotAgent();
        agent.attach(pid);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Future<?> submit = executor.submit(() -> {
            try {
                VM.getVM().getSystemDictionary().allClassesDo(new InvocationCounterVisitor(config));
            } catch (Exception e) {
                reportError(null, e);
            }
        });

        try {
            submit.get(timeout, TimeUnit.SECONDS);
        } catch (Exception ex) {
            reportError(null, ex);
        } finally {
            submit.cancel(true);
            agent.detach();
        }
        closeWriter();
        executor.shutdownNow();
    }

    class InvocationCounterVisitor implements SystemDictionary.ClassVisitor {
        private final Config config;

        public InvocationCounterVisitor(Config config) {
            this.config = config;
        }

        @Override
        public void visit(Klass klass) {
            processSingleKlass(klass, config);
        }
    }

    private void processSingleKlass(Klass klass, Config config) {
        if (!(klass instanceof InstanceKlass)) {
            return;
        }

        String className = klass.getName().asString();
        // Limit the scope of class names to reduce additional losses
        if ((StringUtils.isNotBlank(config.filterPrefix) && !className.startsWith(config.filterPrefix)) || className.contains("Lambda") || className.contains("CGLIB")) {
            return;
        }

        MethodArray methods = ((InstanceKlass) klass).getMethods();
        for (int i = 0; i < methods.getLength(); i++) {
            processSingleMethod(methods.at(i), className, config);
        }
    }

    private void processSingleMethod(Method method, String className, Config config) {
        if (method == null) {
            return;
        }

        String methodName = method.getName().asString();
        if (methodName.contains("lambda") || methodName.contains("init>")) {
            return;
        }

        // 这个操作很关键，上面介绍过这个点：_counter包括三部分：
        // - 第3-31位表示执行次数
        // - 第2位表示是否已被编译1为编译
        // - 第0位和第1位表示超出阈值时的处理，默认情况为01即超出阈值执行编译
        // 右移三位的目的是统计出执行次数信息
        long invocationCount = method.getInvocationCount() >> 3;

        String signature = method.getSignature().asString();

        MethodInfo methodInfo = new MethodInfo(className, methodName, signature, invocationCount);
        String result = methodInfo.toString();
        try {
            if (writer != null) {
                writer.write(result);
                if (config.bufferSize < 0) {
                    writer.flush();
                }
            } else {
                reportError("PrintWriter is closed or not yet initialized, unable to log [" + result + "]", null);
            }
        } catch (Exception e) {
            reportError(null, e);
        }
    }

    private void openWriter(Config config) {
        if (writer != null) {
            return;
        }

        File dir = new File(config.directory);

        // 创建文件夹（如果需要）
        if (!dir.mkdirs() && !dir.isDirectory()) {
            reportError("无法创建 [" + dir + "]", null);
            writer = null;
            return;
        }

        FileOutputStream fos = null;
        OutputStream os = null;
        try {

            File pathName = new File(dir.getAbsoluteFile(), "sa_" + config.time + ".csv");

            fos = new FileOutputStream(pathName, true);
            os = config.bufferSize > 0 ? new BufferedOutputStream(fos, config.bufferSize) : fos;

            writer = new PrintWriter(os, false);

            // write head
            writer.write(MethodInfo.getHead());

        } catch (FileNotFoundException e) {
            reportError(null, e);
            writer = null;
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    // Ignore
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    // Ignore
                }
            }
        }
    }

    private void closeWriter() {
        if (writer == null) {
            return;
        }

        try {
            if (writer == null) {
                return;
            }

            writer.flush();
            writer.close();
            writer = null;
        } catch (Exception e) {
            reportError(null, e);
        }
    }

    static void reportError(String msg, Exception ex) {
        System.err.println(msg);
        if (Objects.nonNull(ex)) {
            ex.printStackTrace();
        }
    }

    public static String getDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    private static String getDefaultDirectory() {
        return "conch_sa_v8";
    }

    private static void deleteOldFiles(Config config) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(FileSystems.getDefault().getPath(config.directory))) {
            for (Path file : files) {
                Files.delete(file);
            }
        } catch (Exception e) {
            reportError("Unable to delete old files", null);
        }
    }

    static class Config {

        String time = getDate(new Date(), "yyyyMMddHHmmss");

        /**
         * SA target PID of Java
         */
        int pid;

        /**
         * SA Count 获取 超时时间 单位：秒
         */
        long timeout;

        /**
         * SA 类名前缀过滤
         */
        String filterPrefix;

        /**
         * SA 类名前缀排除
         * 多个用 , 分隔
         */
        String excludePrefix;

        /**
         * 存放目录
         */
        String directory;

        /**
         * File Buffer Size
         */
        Integer bufferSize;

        public Config(int pid, long timeout, String filterPrefix, String excludePrefix, String directory, Integer bufferSize) {
            this.pid = pid;
            this.timeout = timeout;
            this.filterPrefix = filterPrefix;
            this.excludePrefix = excludePrefix;
            this.directory = StringUtils.isNotBlank(directory) ? directory : getDefaultDirectory();
            this.bufferSize = Objects.nonNull(bufferSize) ? bufferSize : 0;
        }
    }

    static class MethodInfo {

        /**
         * 类名
         */
        final String className;

        /**
         * 方法名
         */
        final String methodName;

        /**
         * 方法签名 - 参数信息
         */
        final String signature;

        /**
         * 方法调用次数
         */
        final long invocationCount;

        public MethodInfo(String className, String methodName, String signature, long invocationCount) {
            this.className = className;
            this.methodName = methodName;
            this.signature = signature;
            this.invocationCount = invocationCount;
        }

        public static String getHead() {
            List<String> fieldNames = new ArrayList<>();
            for (Field field : MethodInfo.class.getDeclaredFields()) {
                fieldNames.add(field.getName());
            }
            return String.join(",", fieldNames) + System.lineSeparator();
        }

        @Override
        public String toString() {
            return className + "," +
                    methodName + "," +
                    signature + "," +
                    invocationCount +
                    System.lineSeparator();
        }
    }
}
