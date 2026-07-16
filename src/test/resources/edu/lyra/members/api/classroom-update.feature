Feature: Classroom updates

    In order to keep classroom records accurate
    As Lyra
    I want to update a classroom's details

    Background:
        Given a school named "Gloria Fuertes" exists
        And a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And a teacher named "Marta" "Ibáñez" exists at school "Gloria Fuertes" with e-mail "marta.ibanez@example.com"

    Scenario: An admin can update a classroom
        Given I am authenticated as an admin with "classrooms.update" scope
        When I update the classroom's course to 4 and group to "B"
        Then I receive a confirmation that the classroom has been successfully updated

    Scenario: A classroom's current tutor can update the classroom
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        And I am authenticated as teacher "Marta Ibáñez" with "classrooms.update" scope
        When I update the classroom's course to 4 and group to "B"
        Then I receive a confirmation that the classroom has been successfully updated

    Scenario: A teacher who is not the classroom's tutor cannot update the classroom
        Given I am authenticated as teacher "Marta Ibáñez" with "classrooms.update" scope
        When I update the classroom's course to 4 and group to "B"
        Then I receive a forbidden error

    Scenario: A parent cannot update a classroom
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And I am authenticated as "esteban.cristobal@example.com" with "classrooms.update" scope
        When I update the classroom's course to 4 and group to "B"
        Then I receive a forbidden error

    Scenario: Cannot update a classroom that does not exist
        Given I am authenticated as an admin with "classrooms.update" scope
        When I update a classroom that does not exist
        Then I receive a not found error
