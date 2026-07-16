Feature: Kid record updates

    In order to keep kid records accurate
    As a parent, tutor, or Lyra administrator
    I want to update a kid's record

    Background:
        Given a school named "Gloria Fuertes" exists
        And the following teachers exist at school "Gloria Fuertes":
            | name  | surname | mail                    |
            | Pablo | Ruiz    | pablo.ruiz@example.com  |
            | José  | García  | jose.garcia@example.com |
        And a classroom for course 1 group "A" exists at school "Gloria Fuertes"
        And teacher "Pablo Ruiz" has been set as the classroom's tutor
        And teacher "José García" has been added to the classroom
        And the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
            | Marta   | Ibáñez    | marta.ibanez@example.com      |
        And the following kids exist:
            | name   | surname   | birthdate  | parent            |
            | Alicia | Cristóbal | 2019-12-12 | Esteban Cristóbal |
            | Fabio  | Ibáñez    | 2019-06-01 | Marta Ibáñez      |
        And kid "Alicia" "Cristóbal" is enrolled in classroom for course 1 group "A"

    Scenario: An admin can update any kid
        Given I am authenticated as an admin with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a confirmation that the kid has been successfully updated

    Scenario: A parent can update their own kid
        Given I am authenticated as "esteban.cristobal@example.com" with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a confirmation that the kid has been successfully updated

    Scenario: A parent cannot update another parent's kid
        Given I am authenticated as "marta.ibanez@example.com" with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a forbidden error

    Scenario: A classroom's tutor can update a kid in that classroom
        Given I am authenticated as teacher "Pablo Ruiz" with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a confirmation that the kid has been successfully updated

    Scenario: A teacher who only teaches (but does not tutor) the classroom cannot update a kid in it
        Given I am authenticated as teacher "José García" with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a forbidden error

    Scenario: A teacher of an unrelated classroom cannot update a kid
        Given a teacher named "Laura" "Gómez" exists at school "Gloria Fuertes" with e-mail "laura.gomez@example.com"
        And I am authenticated as teacher "Laura Gómez" with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a forbidden error

    Scenario: Cannot update a kid that does not exist
        Given I am authenticated as an admin with "kids.update" scope
        When I update a kid that does not exist
        Then I receive a not found error

    Scenario: Cannot update a kid's record when the surname is left blank
        Given I am authenticated as an admin with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s surname to ""
        Then I receive an error stating that "surname" field is incorrect because "must not be blank"

    Scenario: A parent cannot rebind their kid to another parent via the kid update endpoint
        Given I am authenticated as "esteban.cristobal@example.com" with "kids.update" scope
        When I update kid "Alicia" "Cristóbal"'s parent to "Marta Ibáñez"
        Then I receive a forbidden error

    Scenario: A teacher cannot enroll a kid into a classroom they do not tutor via the kid update endpoint
        Given a classroom for course 2 group "B" exists at school "Gloria Fuertes"
        And I am authenticated as teacher "José García" with "kids.update" scope
        When I update kid "Fabio" "Ibáñez"'s classroom to course 2 group "B"
        Then I receive a forbidden error

    Scenario: The target classroom's tutor can enroll a kid via the kid update endpoint
        Given I am authenticated as teacher "Pablo Ruiz" with "kids.update" scope
        When I update kid "Fabio" "Ibáñez"'s classroom to course 1 group "A"
        Then I receive a confirmation that the kid has been successfully updated
