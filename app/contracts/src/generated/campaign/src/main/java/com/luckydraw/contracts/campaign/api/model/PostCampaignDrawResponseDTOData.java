package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.BatchDrawResourceDTO;
import com.luckydraw.contracts.campaign.api.model.DrawResultResourceDTO;
import com.luckydraw.contracts.campaign.api.model.DrawResultType;
import com.luckydraw.contracts.campaign.api.model.PrizeSummaryResourceDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;


@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public interface PostCampaignDrawResponseDTOData {
}
