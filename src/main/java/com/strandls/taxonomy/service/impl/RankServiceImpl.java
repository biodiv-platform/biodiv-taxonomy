package com.strandls.taxonomy.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.strandls.taxonomy.dao.RankDao;
import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.service.RankSerivce;
import com.strandls.taxonomy.util.AbstractService;

public class RankServiceImpl extends AbstractService<Rank> implements RankSerivce {
	
	@Inject
	private RankDao rankDao;
	
	@Inject
	public RankServiceImpl(RankDao dao) {
		super(dao);
	}
	
	@Override
	public Rank fetchById(Long id) {
		return rankDao.findById(id);
	}

	@Override
	public List<Rank> getAllRank(HttpServletRequest request) {
		return rankDao.getAllRank();
	}
	
	@Override
	public List<String> getAllRankNames() {
		List<Rank> ranks = rankDao.getAllRank();
		List<String> rankNames = new ArrayList<>();
		for(Rank rank : ranks) {
			rankNames.add(rank.getName());
		}
		return rankNames;
	}
	
	@Override
	public List<String> getAllRequiredRanks() {
		List<Rank> ranks = rankDao.getAllRank();
		List<String> rankNames = new ArrayList<>();
		for(Rank rank : ranks) {
			if(rank.getIsRequired().booleanValue())
				rankNames.add(rank.getName());
		}
		return rankNames;
	}
	
	@Override
	public Rank addRequiredRank(HttpServletRequest request, String rankName, Double rankValue) {
		Rank rank = new Rank();
		rank.setIsRequired(true);
		rank.setName(rankName);
		rank.setRankValue(rankValue);
		rank.setIsDeleted(false);
		return rankDao.save(rank);
	}

	@Override
	public Rank addIntermediateRank(HttpServletRequest request, String rankName, String highRankName,
			String lowRankName) {
		
		Rank highRank = rankDao.findRankByName(highRankName);
		Rank lowRank = rankDao.findRankByName(lowRankName);
		
		if(highRank == null || lowRank == null) 
			return null;
		
		double rankValue = highRank.getRankValue() + lowRank.getRankValue();
		rankValue /= 2.0;
		
		Rank rank = new Rank();
		rank.setIsRequired(false);
		rank.setName(rankName);
		rank.setRankValue(rankValue);
		rank.setIsDeleted(false);
		return rankDao.save(rank);
	}
}
