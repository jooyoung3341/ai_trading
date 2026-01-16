package com.kr.at.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.kr.at.model.Ticker;
import com.kr.at.service.BinanceService;



@RestController
public class BinanceController {

	@Autowired
	private BinanceService binanceService;
	
	@ResponseBody
	@RequestMapping(value="/allTicket", method=RequestMethod.GET)
	public List<Ticker> allTicket() {
		return binanceService.getTickers();
	}
}
