package com.luckydraw.campaign.repository;

import com.luckydraw.campaign.model.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findAllByOrderByStartTimeAsc();
}
