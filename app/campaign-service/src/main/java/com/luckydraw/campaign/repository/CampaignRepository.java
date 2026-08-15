package com.luckydraw.campaign.repository;

import com.luckydraw.campaign.model.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> {

    List<CampaignEntity> findAllByOrderByStartTimeAsc();
}
