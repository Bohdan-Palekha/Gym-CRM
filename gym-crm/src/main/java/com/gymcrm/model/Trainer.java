package com.gymcrm.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

//trainer IS-A user via JOINED table inheritance
//user_id is pk of trainer and fk to users.id
//ManyToOne to trainingType
//ManyToMany to trainees
@Entity
@Table(name = "trainer")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = "trainees")
public class Trainer extends User {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "specialization_id", nullable = false)
    private TrainingType specialization;

    @ManyToMany(mappedBy = "trainers", fetch = FetchType.LAZY)
    private Set<Trainee> trainees = new HashSet<>();

    public Long getUserId() {
        return getId();
    }
}
