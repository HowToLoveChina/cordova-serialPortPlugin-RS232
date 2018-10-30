package com.running.entity;

import java.io.Serializable;

public class KqInfor implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String studentUuid;
    private String dateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStudentUuid() {
        return studentUuid;
    }

    public void setStudentUuid(String studentUuid) {
        this.studentUuid = studentUuid;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
