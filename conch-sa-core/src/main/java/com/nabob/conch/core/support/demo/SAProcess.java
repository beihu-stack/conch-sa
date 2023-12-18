package com.nabob.conch.core.support.demo;

import com.nabob.conch.core.support.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.code.CodeBlob;
import sun.jvm.hotspot.code.CodeCacheVisitor;
import sun.jvm.hotspot.code.NMethod;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.Method;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.utilities.MethodArray;

import java.util.HashSet;
import java.util.Set;

/**
 * Date:Create at 2020/12/16 17:36 
 * Description:目标进程代码，目的是观察对象内存分布 
 * 主要逻辑： 
 * 1）创建一个Student对象，并调用setId设置id值 
 * 2）对测试的三个方法执行100、1000、10000次的调用 
 * 
 * @author zhichao.pan 
 */
public class SAProcess {

    /*

    "C:\Program Files\Java\jdk1.8.0_191\bin\java.exe" -javaagent:D:\Users\jz.zheng\AppData\Local\JetBrains\Toolbox\apps\IDEA-U\ch-0\231.9161.38\lib\idea_rt.jar=61127:D:\Users\jz.zheng\AppData\Local\JetBrains\Toolbox\apps\IDEA-U\ch-0\231.9161.38\bin -Dfile.encoding=UTF-8 -classpath "C:\Program Files\Java\jdk1.8.0_191\jre\lib\charsets.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\deploy.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\access-bridge-64.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\cldrdata.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\dnsns.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\jaccess.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\jfxrt.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\localedata.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\nashorn.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\sunec.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\sunjce_provider.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\sunmscapi.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\sunpkcs11.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\ext\zipfs.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\javaws.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\jce.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\jfr.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\jfxswt.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\jsse.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\management-agent.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\plugin.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\resources.jar;C:\Program Files\Java\jdk1.8.0_191\jre\lib\rt.jar;C:\Program Files\Java\jdk1.8.0_191\lib\tools.jar;C:\Program Files\Java\jdk1.8.0_191\lib\sa-jdi.jar;D:\conch\conch-sa\conch-sa-core\target\classes;D:\maven\repository\com\fasterxml\jackson\core\jackson-core\2.10.3\jackson-core-2.10.3.jar;D:\maven\repository\com\fasterxml\jackson\core\jackson-annotations\2.10.3\jackson-annotations-2.10.3.jar;D:\maven\repository\com\fasterxml\jackson\core\jackson-databind\2.10.3\jackson-databind-2.10.3.jar;D:\maven\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.10.3\jackson-datatype-jsr310-2.10.3.jar;D:\maven\repository\org\apache\commons\commons-lang3\3.12.0\commons-lang3-3.12.0.jar;D:\maven\repository\org\apache\commons\commons-collections4\4.4\commons-collections4-4.4.jar;D:\maven\repository\org\apache\commons\commons-configuration2\2.8.0\commons-configuration2-2.8.0.jar;D:\maven\repository\org\apache\commons\commons-text\1.9\commons-text-1.9.jar;D:\maven\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar" com.nabob.conch.core.demo.SAProcess
SA遍历方法执行信息：[{"className":"com/nabob/conch/core/TargetProcess$Student","methodName":"JITTest1","parameters":"()V","invocationCount":100},{"className":"com/nabob/conch/core/TargetProcess$Student","methodName":"JITTest2","parameters":"()V","invocationCount":262},{"className":"com/nabob/conch/core/TargetProcess$Student","methodName":"JITTest3","parameters":"()V","invocationCount":264},{"className":"com/nabob/conch/core/TargetProcess$Student","methodName":"<init>","parameters":"()V","invocationCount":1},{"className":"com/nabob/conch/core/TargetProcess$Student","methodName":"setId","parameters":"(I)V","invocationCount":1},{"className":"com/nabob/conch/core/TargetProcess$Student","methodName":"<clinit>","parameters":"()V","invocationCount":1}]
java.lang.RuntimeException: Couldn't deduce type of CodeBlob @0x000000000302aad0 for PC=0x000000000302aad0
	at sun.jvm.hotspot.code.CodeCache.findBlobUnsafe(CodeCache.java:119)
	at sun.jvm.hotspot.code.CodeCache.iterate(CodeCache.java:179)
	at com.nabob.conch.core.demo.SAProcess.main(SAProcess.java:41)
Caused by: sun.jvm.hotspot.types.WrongTypeException: No suitable match for type of address 0x000000000302aad0 (nearest symbol is jvm!??_7VtableBlob@@6B@)
	at sun.jvm.hotspot.runtime.InstanceConstructor.newWrongTypeException(InstanceConstructor.java:62)
	at sun.jvm.hotspot.runtime.VirtualConstructor.instantiateWrapperFor(VirtualConstructor.java:80)
	at sun.jvm.hotspot.code.CodeCache.findBlobUnsafe(CodeCache.java:102)
	... 2 more
SA遍历热编译数据:[{"className":"com.nabob.conch.core.demo.TargetProcess$Student","methodName":"JITTest3","parameters":"()Vsun.jvm.hotspot.oops.Symbol@0x000000002a4485e0","invocationCount":264},{"className":"com.nabob.conch.core.demo.TargetProcess$Student","methodName":"JITTest2","parameters":"()Vsun.jvm.hotspot.oops.Symbol@0x000000002a4485c0","invocationCount":262},{"className":"com.nabob.conch.core.demo.TargetProcess$Student","methodName":"JITTest3","parameters":"()Vsun.jvm.hotspot.oops.Symbol@0x000000002a4485e0","invocationCount":264}]

Process finished with exit code 0


     */
    public static void main(String[] args) {
        int pid = 2808 ;
        HotSpotAgent agent = new HotSpotAgent();
        //对目标进程执行SA        
        agent.attach(pid);
        
        try {
            final Set<MethodDefinition> methodResult = new HashSet<>();            
            VM.getVM().getSystemDictionary().allClassesDo(new InvocationCounterVisitor(methodResult));            
            System.out.println("SA遍历方法执行信息：" + JsonUtil.object2Json(methodResult));
            final Set<MethodDefinition> compiledMethodResult = new HashSet<>();
            VM.getVM().getCodeCache().iterate(new CompiledMethodVisitor(compiledMethodResult));
            System.out.println("SA遍历热编译数据:" + JsonUtil.object2Json(compiledMethodResult));
        } finally {            
        //释放SA            
        agent.detach();        
        }    
    }
    
    //SA获取方法调用数据    
    static class InvocationCounterVisitor implements SystemDictionary.ClassVisitor {
    private final Set<MethodDefinition> result;        
    public InvocationCounterVisitor(Set<MethodDefinition> result) {            
            this.result = result;       
    }       
    @Override    
    public void visit(Klass klass) {
            final String klassName = klass.getName().asString();//类全限定名            
            if (klassName.contains("Student")) { //此处只关注目标进程的Student类                
                final MethodArray methods = ((InstanceKlass) klass).getMethods();//该类下的方法
                for (int i = 0; i < methods.length(); i++) {                    
                    final Method method = methods.at(i);
                    long invocationCount = method.getInvocationCount(); //遍历获取执行次出                    
                    //这个操作很关键，上面介绍过这个点：_counter包括三部分：                    
                    // 第3-31位表示执行次数，第2位表示是否已被编译1为编译，第0位和第1位表示超出阈值时的处理，默认情况为01即超出阈值执行编译                    
                    // 右移三位的目的是统计出执行次数信息                    
                    invocationCount = invocationCount >> 3;                    
                    result.add(                            
                            new MethodDefinition(klassName, method.getName().asString(),                                    
                                    method.getSignature().asString(),                                    
                                    invocationCount));                
                }            
            }        
        }
    }

    //SA获取已被JIT编译的类及方法信息    
    static class CompiledMethodVisitor implements CodeCacheVisitor {
        private final Set<MethodDefinition> result;        
        @Override        
        public void visit(CodeBlob codeBlob) {//codeBlob为jit编译后的代码在内存中的对象表示
            final NMethod nMethod = codeBlob.asNMethodOrNull();
            if(nMethod == null ) return;            
            final Method method = nMethod.getMethod();            
            final String className = method.getMethodHolder().getName().asString();//类名            
            final String name = method.getName().asString();//方法名
            final String signature = method.getSignature().asString();//方法参数
            long invocationCount = method.getInvocationCount();//调用次数            
            //右移三位的目的同InvocationCounterVisitor            
            invocationCount = invocationCount >> 3;            
            if(className.contains("Student")){                
                result.add(new MethodDefinition(StringUtils.replace(className, "/", "."), name,
                        signature + method.getName(), invocationCount));
            }        
        }        
        @Override        
        public void epilogue() {        
        }        
        public CompiledMethodVisitor(Set<MethodDefinition> result) {            
            this.result = result;       
        }        
        @Override        
        public void prologue(Address address, Address address1) {
        }  
    }

    static class MethodDefinition {        
        public String className;//类名    
        public String methodName;//方法名   
        public String parameters;//方法参数      
        public long invocationCount;//方法调用次数

        public MethodDefinition(String className, String methodName, String parameters, long invocationCount) {            
            this.className = className;            
            this.methodName = methodName;      
            this.parameters = parameters;         
            this.invocationCount = invocationCount;        
        }    
    }
}