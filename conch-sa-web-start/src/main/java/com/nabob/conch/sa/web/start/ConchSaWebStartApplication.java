package com.nabob.conch.sa.web.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConchSaWebStartApplication {

    public static void main(String[] args) {
//        if (args.length <= 1) {
//            System.out.println("Usage: java AttachTest <PID> /PATH/TO/AGENT.jar");
//            return;
//        }

//        VirtualMachine vm = VirtualMachine.attach(args[0]);

        SpringApplication.run(ConchSaWebStartApplication.class, args);

//        vm.loadAgent(args[1]);
    }

}
