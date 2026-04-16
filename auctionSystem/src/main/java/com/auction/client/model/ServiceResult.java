package com.auction.client.model;
public record ServiceResult<T>(boolean success, String message, T data) {}
