package com.lms.service;

/** Implemented by services that can report how many records they manage, so dashboards can treat them uniformly. */
public interface Countable {

    int countAll();
}
