/** */
package com.strandls.taxonomy.service;

import com.strandls.taxonomy.pojo.Rank;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author vilay
 */
public interface RankSerivce {

  public Rank fetchById(Long id);

  public Rank addRequiredRank(HttpServletRequest request, String rankName, Double rankValue);

  public Rank addIntermediateRank(
      HttpServletRequest request, String rankName, String highRankName, String lowRankName);

  public List<Rank> getAllRank(HttpServletRequest request);

  public List<String> getAllRankNames();

  public List<String> getAllRequiredRanks();
}
