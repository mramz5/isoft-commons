package com.isoft.commons.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseModel implements Model, Serializable {

    private Long id;
    private String createdUser;
    private LocalDate createdDate;
    private String updatedUser;
    private LocalDate updatedDate;
}
