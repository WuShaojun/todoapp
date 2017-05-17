/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksPersistenceContract.TaskEntry;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Concrete implementation of a data source as a db.
 */
public class TasksLocalDataSource implements TasksDataSource {

    @Nullable
    private static TasksLocalDataSource INSTANCE;

//    @NonNull
//    private final BriteDatabase mDatabaseHelper;

    @NonNull
    private Function<Cursor, Task> mTaskMapperFunction;
    private final TasksDbHelper mDbHelper;

    // Prevent direct instantiation.
    private TasksLocalDataSource(@NonNull Context context,
                                 @NonNull BaseSchedulerProvider schedulerProvider) {
        checkNotNull(context, "context cannot be null");
        checkNotNull(schedulerProvider, "scheduleProvider cannot be null");
        mDbHelper = new TasksDbHelper(context);

//        SqlBrite sqlBrite = SqlBrite.create();
//        mDatabaseHelper = sqlBrite.wrapDatabaseHelper(mDbHelper, schedulerProvider.io());
        mTaskMapperFunction = this::getTask;
    }

    @NonNull
    private Task getTask(@NonNull Cursor c) {

        String itemId = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_ENTRY_ID));
        String title = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TITLE));
        String description =
                c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_DESCRIPTION));
        boolean completed =
                c.getInt(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_COMPLETED)) == 1;
        return new Task(title, description, itemId, completed);
    }

    public static TasksLocalDataSource getInstance(
            @NonNull Context context,
            @NonNull BaseSchedulerProvider schedulerProvider) {
        if (INSTANCE == null) {
            INSTANCE = new TasksLocalDataSource(context, schedulerProvider);
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    @Override
    public Observable<List<Task>> getTasks() {
        String[] projection = {
                TaskEntry.COLUMN_NAME_ENTRY_ID,
                TaskEntry.COLUMN_NAME_TITLE,
                TaskEntry.COLUMN_NAME_DESCRIPTION,
                TaskEntry.COLUMN_NAME_COMPLETED
        };
        String sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), TaskEntry.TABLE_NAME);
//        return mDatabaseHelper.createQuery(TaskEntry.TABLE_NAME, sql)
//                .mapToList(mTaskMapperFunction);

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                TaskEntry.TABLE_NAME,
                projection,
                null,
                null, null, null, null);
        List<Task> tasks = getTasks(cursor);
        Observable<List<Task>> observable = Observable.just(tasks);
        db.close();
        return observable;
    }

    private List<Task> getTasks(Cursor cursor) {
        List<Task> tasks = new ArrayList<>();
        if (cursor == null) return tasks;
        while (cursor.moveToNext()) {
            Task task = getTask(cursor);
            tasks.add(task);
        }
        return tasks;
    }

    @Override
    public Observable<Task> getTask(@NonNull String taskId) {
        String[] projection = {
                TaskEntry.COLUMN_NAME_ENTRY_ID,
                TaskEntry.COLUMN_NAME_TITLE,
                TaskEntry.COLUMN_NAME_DESCRIPTION,
                TaskEntry.COLUMN_NAME_COMPLETED
        };
//        String sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
//                TextUtils.join(",", projection), TaskEntry.TABLE_NAME, TaskEntry.COLUMN_NAME_ENTRY_ID);
//        return mDatabaseHelper.createQuery(TaskEntry.TABLE_NAME, sql, taskId)
//                .mapToOneOrDefault(mTaskMapperFunction, null);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        //参数1：表名
//参数2：要想显示的列
//参数3：where子句
//参数4：where子句对应的条件值
//参数5：分组方式
//参数6：having条件
//参数7：排序方式
        Cursor cursor = db.query(
                TaskEntry.TABLE_NAME,
                projection,
                TaskEntry.COLUMN_NAME_ENTRY_ID + "=?",
                new String[]{taskId}, null, null, null);
        if (cursor != null && cursor.moveToNext()) {
            Task task = getTask(cursor);
            Observable<Task> observable = Observable.just(task);
            db.close();
            return observable;
        }

        return Observable.empty();
    }

    @Override
    public void saveTask(@NonNull Task task) {
        checkNotNull(task);
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_ENTRY_ID, task.getId());
        values.put(TaskEntry.COLUMN_NAME_TITLE, task.getTitle());
        values.put(TaskEntry.COLUMN_NAME_DESCRIPTION, task.getDescription());
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, task.isCompleted());
//        mDatabaseHelper.insert(TaskEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.insert(TaskEntry.TABLE_NAME, null, values);
        db.close();
    }

    @Override
    public void completeTask(@NonNull Task task) {
        completeTask(task.getId());
    }

    @Override
    public void completeTask(@NonNull String taskId) {
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, true);

        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + "=?";
        String[] selectionArgs = {taskId};
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
        db.close();
//        mDatabaseHelper.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void activateTask(@NonNull Task task) {
        activateTask(task.getId());
    }

    @Override
    public void activateTask(@NonNull String taskId) {
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, false);

        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + "=?";
        String[] selectionArgs = {taskId};
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
        db.close();
//        mDatabaseHelper.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void clearCompletedTasks() {
        String selection = TaskEntry.COLUMN_NAME_COMPLETED + " LIKE ?";
        String[] selectionArgs = {"1"};
//        mDatabaseHelper.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public void refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllTasks() {
//        mDatabaseHelper.delete(TaskEntry.TABLE_NAME, null);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(TaskEntry.TABLE_NAME, null, null);

    }

    @Override
    public void deleteTask(@NonNull String taskId) {
        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {taskId};
//        mDatabaseHelper.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
    }
}
