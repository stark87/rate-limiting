package com.example.rate.limiting.api;

import com.example.rate.limiting.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/data")
@RequiredArgsConstructor
public class Controller {

    private final AppService appService;

    @GetMapping
    public String get(){
        return appService.data();
    }
}
