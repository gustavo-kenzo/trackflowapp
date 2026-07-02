package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor
public class Category extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    public Category(User user, String name) {
        this.user = user;
        this.name = name;
        this.active = true;
    }

    public void update(String name, Boolean active) {
        if (name != null)
            this.name = name;
        if (active != null)
            this.active = active;
    }
}
