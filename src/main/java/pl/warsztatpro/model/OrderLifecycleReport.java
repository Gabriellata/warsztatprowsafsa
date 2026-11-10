package pl.warsztatpro.model;

public record OrderLifecycleReport(long total, long newOrders, long inProgress, long completed, long overdue, long cancelled) { }
