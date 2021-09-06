/**
 * 
 */
package com.strandls.taxonomy.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.strandls.taxonomy.pojo.Rank;

/**
 * 
 * @author vilay
 *
 */
public interface RankSerivce {

	public Rank fetchById(Long id);

	public Rank addRequiredRank(HttpServletRequest request, String rankName, Double rankValue);

	public Rank addIntermediateRank(HttpServletRequest request, String rankName, String highRankName,
			String lowRankName);

	public List<Rank> getAllRank(HttpServletRequest request);

	public List<String> getAllRankNames();
	
	public List<String> getAllRequiredRanks();
}
