package com.heller.mockmvc.mocktest.controller;

import com.heller.mockmvc.mocktest.bo.Hello;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {
    
    @RequestMapping("/sayHello")
    public Hello sayHello(@RequestParam String name) {
        Hello hello = new Hello();
        hello.setName(name);
        hello.setWord("Hello.");
        
        return hello;
    }
    
}
