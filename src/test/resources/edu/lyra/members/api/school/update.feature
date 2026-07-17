Feature: School updates

    In order to keep school records accurate
    As Lyra
    I want to update a school's details

    Background:
        Given a school named "Gloria Fuertes" exists

    Scenario: An admin can update a school
        Given I am authenticated as an admin with "schools.update" scope
        When I update school "Gloria Fuertes"'s name to "Gloria Fuertes Norte"
        Then I receive a confirmation that the school has been successfully updated

    Scenario: A parent cannot update a school
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And I am authenticated as "esteban.cristobal@example.com" with "schools.update" scope
        When I update school "Gloria Fuertes"'s name to "Gloria Fuertes Norte"
        Then I receive a forbidden error

    Scenario: A teacher cannot update a school
        Given a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Pablo Ruiz" with "schools.update" scope
        When I update school "Gloria Fuertes"'s name to "Gloria Fuertes Norte"
        Then I receive a forbidden error

    Scenario: Cannot update a school that does not exist
        Given I am authenticated as an admin with "schools.update" scope
        When I update a school that does not exist
        Then I receive a not found error

    Scenario: Cannot update a school's name to blank
        Given I am authenticated as an admin with "schools.update" scope
        When I update school "Gloria Fuertes"'s name to ""
        Then I receive an error stating that "name" field is incorrect because "must not be blank"
