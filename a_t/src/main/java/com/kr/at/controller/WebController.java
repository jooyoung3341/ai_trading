package com.kr.at.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class WebController {

	@RequestMapping(value="/", method=RequestMethod.GET)
	public String home(Model model) {
		return "web/main";
	}
	
	@RequestMapping(value="/web/learning", method=RequestMethod.GET)
	public String learningMain(Model model) {
		return "web/learning";
	}
	
	@RequestMapping(value="/web/predict", method=RequestMethod.GET)
	public String predict(Model model) {
		return "web/predict";
	}
	
}
