package com.bankdemo.cashmanagementservice.dto;

/** Mirrors cash-requirement-service's SuggestedSourceDto — the nearest surplus branch that could
 * fund a cash-logistics transfer into a branch projected to be at risk. */
public class SuggestedSourceDto {
    private String branchId;
    private String branchName;
    public String getBranchId() { return branchId; }
    public void setBranchId(String value) { branchId = value; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String value) { branchName = value; }
}
