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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CategoryType type;

    @Column(name = "system_default", nullable = false)
    private boolean systemDefault;

    @Column(name = "active", nullable = false)
    private boolean active;

    public Category(User user, String name, CategoryType type, Boolean systemDefault) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.systemDefault = systemDefault != null ? systemDefault : false;
        this.active = true;
    }

    public void deactivate() {
        if (!this.systemDefault)
            this.active = false;
        else
            throw new IllegalStateException("system category cannot be deactivated");
    }

    public void activate() {
        this.active = true;
    }

    public boolean updateName(String name) {
        if (name != null) {
            this.name = name;
            return true;
        }
        return false;
    }

    public boolean updateType(CategoryType type) {
        if (type != null && (type.equals(CategoryType.INCOME) || type.equals(CategoryType.EXPENSE))) {
            this.type = type;
            return true;
        }
        return false;
    }

    public boolean updateSystemDefault(Boolean systemDefault) {
        if (systemDefault != null) {
            this.systemDefault = systemDefault;
            return true;
        }
        return false;
    }

    public boolean updateActive(Boolean active) {
        if (active != null) {
            this.active = active;
            return true;
        }
        return false;
    }
}
