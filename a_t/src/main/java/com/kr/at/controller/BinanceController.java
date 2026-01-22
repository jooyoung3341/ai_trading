package com.kr.at.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
	
	@GetMapping("at/allTicket")
	public Map<String, Object> atAllTicket(){
		List<Ticker> ticketList = binanceService.getTickers();
		List<String> symbolList = new ArrayList<>();
		for (Ticker t : ticketList) {
			symbolList.add(t.getSymbol());
		}
		return Map.of("symbols", symbolList);
	}
}
