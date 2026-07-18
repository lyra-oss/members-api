Feature: Classroom deletion

    In order to keep classroom records accurate
    As a tutor or Lyra administrator
    I want to delete a classroom

    Background:
        Given a school named "Gloria Fuertes" exists
        And a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And a teacher named "Marta" "Ibáñez" exists at school "Gloria Fuertes" with e-mail "marta.ibanez@example.com"

    Scenario: An admin can delete a classroom without kids
        Given I am authenticated as an admin with "classrooms.delete" scope
        When I delete the classroom
        Then I receive a confirmation that the classroom has been successfully deleted

    Scenario: The classroom's current tutor can delete it
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        And I am authenticated as teacher "Marta Ibáñez" with "classrooms.delete" scope
        When I delete the classroom
        Then I receive a confirmation that the classroom has been successfully deleted

    Scenario: A teacher who is not the classroom's tutor cannot delete the classroom
        Given I am authenticated as teacher "Marta Ibáñez" with "classrooms.delete" scope
        When I delete the classroom
        Then I receive a forbidden error

    Scenario: A parent cannot delete a classroom
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And I am authenticated as "esteban.cristobal@example.com" with "classrooms.delete" scope
        When I delete the classroom
        Then I receive a forbidden error

    Scenario: Cannot delete a classroom that does not exist
        Given I am authenticated as an admin with "classrooms.delete" scope
        When I delete a classroom that does not exist
        Then I receive a not found error

    Scenario: An admin cannot delete a classroom that still has kids
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And the following kids exist:
            | name   | surname   | birthdate  | parent            |
            | Alicia | Cristóbal | 2019-12-12 | Esteban Cristóbal |
        And kid "Alicia" "Cristóbal" is enrolled in classroom for course 3 group "A"
        And I am authenticated as an admin with "classrooms.delete" scope
        When I delete the classroom
        Then I receive a conflict error

    Scenario: The tutor cannot delete their classroom while it still has kids
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        And the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And the following kids exist:
            | name   | surname   | birthdate  | parent            |
            | Alicia | Cristóbal | 2019-12-12 | Esteban Cristóbal |
        And kid "Alicia" "Cristóbal" is enrolled in classroom for course 3 group "A"
        And I am authenticated as teacher "Marta Ibáñez" with "classrooms.delete" scope
        When I delete the classroom
        Then I receive a conflict error

    Scenario: A classroom with teachers but no kids can be deleted
        Given teacher "Marta Ibáñez" has been added to the classroom
        And I am authenticated as an admin with "classrooms.delete" scope
        When I delete the classroom
        Then I receive a confirmation that the classroom has been successfully deleted
