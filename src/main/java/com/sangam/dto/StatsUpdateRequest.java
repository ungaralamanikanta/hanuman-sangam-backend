package com.sangam.dto;

public class StatsUpdateRequest {
    private Boolean autoCalculate       = true;
    private Double  totalCollection;
    private Double  totalPaid;
    private Double  totalUnpaid;
    private Integer totalPendingMembers;

    public Boolean getAutoCalculate()                   { return autoCalculate; }
    public void setAutoCalculate(Boolean autoCalculate) { this.autoCalculate = autoCalculate; }
    public Double getTotalCollection()                  { return totalCollection; }
    public void setTotalCollection(Double v)            { this.totalCollection = v; }
    public Double getTotalPaid()                        { return totalPaid; }
    public void setTotalPaid(Double v)                  { this.totalPaid = v; }
    public Double getTotalUnpaid()                      { return totalUnpaid; }
    public void setTotalUnpaid(Double v)                { this.totalUnpaid = v; }
    public Integer getTotalPendingMembers()             { return totalPendingMembers; }
    public void setTotalPendingMembers(Integer v)       { this.totalPendingMembers = v; }
}
