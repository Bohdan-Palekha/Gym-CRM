package com.gymcrm.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

//trainee IS-A user via JOINED table inheritance
//user_id is pk of trainee and fk to users.id
//trainers - ManyToMany(owner, join table trainee_trainer)
//training OneToMany. Deleting trainee causes cascade delete
@Entity
@Table(name = "trainee")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"trainers", "trainings"})
public class Trainee extends User {

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "trainee_trainer",
        joinColumns        = @JoinColumn(name = "trainee_id"),
        inverseJoinColumns = @JoinColumn(name = "trainer_id")
    )
    private Set<Trainer> trainers = new HashSet<>();

    @OneToMany(mappedBy = "trainee",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private Set<Training> trainings = new HashSet<>();

    public Long getUserId() {
        return getId();
    }
}
