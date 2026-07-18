Feature: Kid record deletion

    In order to keep kid records accurate
    As a parent, tutor, or Lyra administrator
    I want to delete a kid's record

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

    Scenario: An admin can delete any kid
        Given I am authenticated as an admin with "kids.delete" scope
        When I delete kid "Alicia" "Cristóbal"
        Then I receive a confirmation that the kid has been successfully deleted

    Scenario: A parent can delete their own kid
        Given I am authenticated as "esteban.cristobal@example.com" with "kids.delete" scope
        When I delete kid "Alicia" "Cristóbal"
        Then I receive a confirmation that the kid has been successfully deleted

    Scenario: A parent cannot delete another parent's kid
        Given I am authenticated as "marta.ibanez@example.com" with "kids.delete" scope
        When I delete kid "Alicia" "Cristóbal"
        Then I receive a forbidden error

    Scenario: A classroom's tutor can delete a kid in that classroom
        Given I am authenticated as teacher "Pablo Ruiz" with "kids.delete" scope
        When I delete kid "Alicia" "Cristóbal"
        Then I receive a confirmation that the kid has been successfully deleted

    Scenario: A teacher who only teaches (but does not tutor) the classroom cannot delete a kid in it
        Given I am authenticated as teacher "José García" with "kids.delete" scope
        When I delete kid "Alicia" "Cristóbal"
        Then I receive a forbidden error

    Scenario: A teacher of an unrelated classroom cannot delete a kid
        Given a teacher named "Laura" "Gómez" exists at school "Gloria Fuertes" with e-mail "laura.gomez@example.com"
        And I am authenticated as teacher "Laura Gómez" with "kids.delete" scope
        When I delete kid "Alicia" "Cristóbal"
        Then I receive a forbidden error

    Scenario: A teacher cannot delete a kid who is not enrolled in any classroom
        Given I am authenticated as teacher "Pablo Ruiz" with "kids.delete" scope
        When I delete kid "Fabio" "Ibáñez"
        Then I receive a forbidden error

    Scenario: Cannot delete a kid that does not exist
        Given I am authenticated as an admin with "kids.delete" scope
        When I delete a kid that does not exist
        Then I receive a not found error
