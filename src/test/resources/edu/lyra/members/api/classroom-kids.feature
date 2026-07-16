Feature: Classroom kid enrollment

    In order to organize kids into classrooms
    As Lyra
    I want to enroll a kid into a classroom

    Background:
        Given a school named "Gloria Fuertes" exists
        And a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And a teacher named "Marta" "Ibáñez" exists at school "Gloria Fuertes" with e-mail "marta.ibanez@example.com"
        And the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And the following kids exist:
            | name   | surname   | birthdate  | parent            |
            | Alicia | Cristóbal | 2019-12-12 | Esteban Cristóbal |

    Scenario: An admin can enroll a kid into a classroom
        Given I am authenticated as an admin with "classrooms.update" scope
        When I enroll kid "Alicia" "Cristóbal" in the classroom
        Then I receive a confirmation that the kid has been successfully enrolled

    Scenario: A classroom's tutor can enroll a kid into their classroom
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        And I am authenticated as teacher "Marta Ibáñez" with "classrooms.update" scope
        When I enroll kid "Alicia" "Cristóbal" in the classroom
        Then I receive a confirmation that the kid has been successfully enrolled

    Scenario: A teacher who is not the classroom's tutor cannot enroll a kid
        Given I am authenticated as teacher "Marta Ibáñez" with "classrooms.update" scope
        When I enroll kid "Alicia" "Cristóbal" in the classroom
        Then I receive a forbidden error

    Scenario: A parent cannot enroll their kid into a classroom
        Given I am authenticated as "esteban.cristobal@example.com" with "classrooms.update" scope
        When I enroll kid "Alicia" "Cristóbal" in the classroom
        Then I receive a forbidden error
