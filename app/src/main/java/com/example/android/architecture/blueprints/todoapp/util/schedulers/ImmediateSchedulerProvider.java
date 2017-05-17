package com.example.android.architecture.blueprints.todoapp.util.schedulers;

import android.support.annotation.NonNull;

import io.reactivex.Scheduler;
import io.reactivex.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.schedulers.Schedulers;


/**
 * Implementation of the {@link BaseSchedulerProvider} making all {@link Scheduler}s immediate.
 */
public class ImmediateSchedulerProvider implements BaseSchedulerProvider {

    @NonNull
    @Override
    public Scheduler computation() {
        return ImmediateThinScheduler.INSTANCE;
    }

    @NonNull
    @Override
    public Scheduler io() {
        return ImmediateThinScheduler.INSTANCE;
    }

    @NonNull
    @Override
    public Scheduler ui() {
        return ImmediateThinScheduler.INSTANCE;
    }
}
