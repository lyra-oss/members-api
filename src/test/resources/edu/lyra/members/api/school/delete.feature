Feature: School deletion

    In order to keep school records accurate
    As Lyra
    I want to delete a school

    Background:
        Given a school named "Gloria Fuertes" exists

    Scenario: An admin can delete a school with no references
        Given I am authenticated as an admin with "schools.delete" scope
        When I delete school "Gloria Fuertes"
        Then I receive a confirmation that the school has been successfully deleted

    Scenario: A parent cannot delete a school
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And I am authenticated as "esteban.cristobal@example.com" with "schools.delete" scope
        When I delete school "Gloria Fuertes"
        Then I receive a forbidden error

    Scenario: A teacher cannot delete a school
        Given a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Pablo Ruiz" with "schools.delete" scope
        When I delete school "Gloria Fuertes"
        Then I receive a forbidden error

    Scenario: Cannot delete a school that does not exist
        Given I am authenticated as an admin with "schools.delete" scope
        When I delete a school that does not exist
        Then I receive a not found error

    Scenario: An admin cannot delete a school that still has classrooms
        Given a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And I am authenticated as an admin with "schools.delete" scope
        When I delete school "Gloria Fuertes"
        Then I receive a conflict error

    Scenario: An admin cannot delete a school that still has teachers
        Given a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as an admin with "schools.delete" scope
        When I delete school "Gloria Fuertes"
        Then I receive a conflict error
