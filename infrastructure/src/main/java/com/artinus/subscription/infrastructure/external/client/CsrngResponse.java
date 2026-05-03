package com.artinus.subscription.infrastructure.external.client;

record CsrngResponse(String status, int min, int max, int random) {
}
