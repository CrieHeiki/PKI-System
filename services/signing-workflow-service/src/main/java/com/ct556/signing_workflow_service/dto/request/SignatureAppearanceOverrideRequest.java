package com.ct556.signing_workflow_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SignatureAppearanceOverrideRequest {

    @Min(value = 1, message = "pageNumber must be at least 1")
    private Integer pageNumber;

    @Min(value = 0, message = "xPercent must be between 0 and 95")
    @Max(value = 95, message = "xPercent must be between 0 and 95")
    private Integer xPercent;

    @Min(value = 0, message = "yPercent must be between 0 and 95")
    @Max(value = 95, message = "yPercent must be between 0 and 95")
    private Integer yPercent;

    @Min(value = 5, message = "widthPercent must be between 5 and 100")
    @Max(value = 100, message = "widthPercent must be between 5 and 100")
    private Integer widthPercent;

    @Min(value = 5, message = "heightPercent must be between 5 and 100")
    @Max(value = 100, message = "heightPercent must be between 5 and 100")
    private Integer heightPercent;

    private Boolean imageOnly;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getXPercent() {
        return xPercent;
    }

    public void setXPercent(Integer xPercent) {
        this.xPercent = xPercent;
    }

    public Integer getYPercent() {
        return yPercent;
    }

    public void setYPercent(Integer yPercent) {
        this.yPercent = yPercent;
    }

    public Integer getWidthPercent() {
        return widthPercent;
    }

    public void setWidthPercent(Integer widthPercent) {
        this.widthPercent = widthPercent;
    }

    public Integer getHeightPercent() {
        return heightPercent;
    }

    public void setHeightPercent(Integer heightPercent) {
        this.heightPercent = heightPercent;
    }

    public Boolean getImageOnly() {
        return imageOnly;
    }

    public void setImageOnly(Boolean imageOnly) {
        this.imageOnly = imageOnly;
    }
}
