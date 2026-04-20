package com.sangam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dashboard_stats")
public class DashboardStats {

    @Id
    private Long id = 1L;   // Always one row

    private Double  totalCollection     = 0.0;
    private Double  totalPaid           = 0.0;
    private Double  totalUnpaid         = 0.0;
    private Integer totalPendingMembers = 0;
    private boolean autoCalculated      = true;

    public Long getId()                           { return id; }
    public void setId(Long id)                    { this.id = id; }
    public Double getTotalCollection()            { return totalCollection; }
    public void setTotalCollection(Double v)      { this.totalCollection = v; }
    public Double getTotalPaid()                  { return totalPaid; }
    public void setTotalPaid(Double v)            { this.totalPaid = v; }
    public Double getTotalUnpaid()                { return totalUnpaid; }
    public void setTotalUnpaid(Double v)          { this.totalUnpaid = v; }
    public Integer getTotalPendingMembers()       { return totalPendingMembers; }
    public void setTotalPendingMembers(Integer v) { this.totalPendingMembers = v; }
    public boolean isAutoCalculated()             { return autoCalculated; }
    public void setAutoCalculated(boolean v)      { this.autoCalculated = v; }
}
