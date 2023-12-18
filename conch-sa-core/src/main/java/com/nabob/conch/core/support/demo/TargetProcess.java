package com.nabob.conch.core.support.demo;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Date:Create at 2020/12/16 17:36
 * Description:目标进程代码，目的是观察方法调用情况
 * 主要逻辑：
 * 1）创建一个Student对象，并调用setId设置id值
 * 2）对测试的三个方法执行100、1000、10000次的调用
 *
 * @author zhichao.pan
 */
public class TargetProcess {
    public static void main(String[] args) throws Exception {
        // get name representing the running Java virtual machine.
        String name = ManagementFactory.getRuntimeMXBean().getName();
        System.out.println(name);
        // get pid
        String pid = name.split("@")[0];
        System.out.println("Pid is:" + pid);

        Thread.sleep(TimeUnit.SECONDS.toMillis(20));

        int id = 1;
        Student student = new Student();
        student.setId(id);
        for (int i = 0; i < 100; i++) {
            student.JITTest1();
        }
        for (int i = 0; i < 1000; i++) {
            student.JITTest2();
        }
        for (int i = 0; i < 100000; i++) {
            student.JITTest3();
        }
        for (int i = 0; i < 100000; i++) {
            student.JITTest4(String.valueOf(new Random().nextInt()));
        }
        //为方便通过SA观察结果，睡眠10000000        
        Thread.sleep(10000000 * 1000);
    }

    static class Student {
        private static int type = 10;
        private int id;

        public void setId(int id) {
            this.id = id;
        }

        void JITTest1() {
            System.out.println(this.id);
        }

        void JITTest2() {
            System.out.println(this.id);
        }

        void JITTest3() {
            System.out.println(this.id);
        }
        void JITTest4(String name) {
            System.out.println(name);
        }
    }
}