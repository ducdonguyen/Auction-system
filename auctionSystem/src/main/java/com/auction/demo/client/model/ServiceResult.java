package com.auction.demo.client.model;
public record ServiceResult<T>(boolean success, String message, T data) {}
