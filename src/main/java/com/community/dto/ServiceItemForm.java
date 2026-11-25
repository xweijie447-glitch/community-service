package com.community.dto;

import com.community.domain.ItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 服务项目表单
 */
@Getter
@Setter
public class ServiceItemForm {

    private Long id;

    @NotNull(message = "请选择所属分类")
    private Long categoryId;

    @NotBlank(message = "服务名称不能为空")
    @Size(max = 80, message = "服务名称不能超过 80 个字")
    private String name;

    @Size(max = 120, message = "卖点描述不能超过 120 个字")
    private String subtitle;

    @Size(max = 2000, message = "服务说明不能超过 2000 个字")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @NotBlank(message = "计价单位不能为空")
    @Size(max = 10, message = "单位不能超过 10 个字")
    private String unit = "次";

    @NotNull(message = "服务时长不能为空")
    @Min(value = 15, message = "服务时长至少 15 分钟")
    private Integer durationMinutes = 60;

    private Boolean hot = Boolean.FALSE;

    private ItemStatus status = ItemStatus.ON_SALE;

    public static ServiceItemForm from(com.community.domain.ServiceItem item) {
        ServiceItemForm form = new ServiceItemForm();
        form.setId(item.getId());
        form.setCategoryId(item.getCategoryId());
        form.setName(item.getName());
        form.setSubtitle(item.getSubtitle());
        form.setDescription(item.getDescription());
        form.setPrice(item.getPrice());
        form.setUnit(item.getUnit());
        form.setDurationMinutes(item.getDurationMinutes());
        form.setHot(item.getHot());
        form.setStatus(item.getStatus());
        return form;
    }
}
