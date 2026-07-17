Feature: Classroom teaching staff

    In order to manage who teaches each classroom
    As Lyra
    I want to assign teachers and a tutor to a classroom

    Background:
        Given I am authenticated with "classrooms.read" scope
        And a school named "Gloria Fuertes" exists
        And a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And a teacher named "Marta" "Ibáñez" exists at school "Gloria Fuertes" with e-mail "marta.ibanez@example.com"

    Scenario: Add a teacher to a classroom
        Given I am authenticated as an admin with "classrooms.update" scope
        When I add teacher "Marta Ibáñez" to the classroom
        Then I receive a confirmation that the teacher has been successfully added to the classroom

    Scenario: Get a classroom's teachers
        Given teacher "Marta Ibáñez" has been added to the classroom
        When I request the classroom's teachers
        Then the classroom's teachers include "Marta Ibáñez"

    Scenario: Set a classroom's tutor
        Given I am authenticated as an admin with "classrooms.update" scope
        When I set teacher "Marta Ibáñez" as the classroom's tutor
        Then I receive a confirmation that the tutor has been successfully set

    Scenario: Get a classroom's tutor
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        When I request the classroom's tutor
        Then the classroom's tutor is "Marta Ibáñez"

    Scenario: Cannot get a classroom's tutor when none has been assigned
        When I request the classroom's tutor
        Then I receive a not found error

    Scenario: Cannot add a teacher from a different school to a classroom
        Given a school named "Montessori Norte" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Montessori Norte" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as an admin with "classrooms.update" scope
        When I add teacher "Pablo Ruiz" to the classroom
        Then I receive an error because the teacher does not belong to the classroom's school

    Scenario: Cannot set a tutor from a different school
        Given a school named "Montessori Norte" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Montessori Norte" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as an admin with "classrooms.update" scope
        When I set teacher "Pablo Ruiz" as the classroom's tutor
        Then I receive an error because the teacher does not belong to the classroom's school

    Scenario: Cannot add a teacher to a classroom that does not exist
        Given I am authenticated as an admin with "classrooms.update" scope
        When I add teacher "Marta Ibáñez" to a classroom that does not exist
        Then I receive a not found error

    Scenario: Cannot create a classroom with a tutor from a different school
        Given a school named "Montessori Norte" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Montessori Norte" with e-mail "pablo.ruiz@example.com"
        When I create a classroom for course 4 group "B" at school "Gloria Fuertes" with tutor "Pablo Ruiz"
        Then I receive an error because the teacher does not belong to the classroom's school

    Scenario: A classroom's current tutor can add another teacher to the classroom
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        And a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Marta Ibáñez" with "classrooms.update" scope
        When I add teacher "Pablo Ruiz" to the classroom
        Then I receive a confirmation that the teacher has been successfully added to the classroom

    Scenario: A classroom's current tutor can reassign the tutor
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        And a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Marta Ibáñez" with "classrooms.update" scope
        When I set teacher "Pablo Ruiz" as the classroom's tutor
        Then I receive a confirmation that the tutor has been successfully set

    Scenario: A teacher who is not the classroom's tutor cannot add a teacher to the classroom
        Given a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Pablo Ruiz" with "classrooms.update" scope
        When I add teacher "Marta Ibáñez" to the classroom
        Then I receive a forbidden error

    Scenario: A teacher who is not the classroom's tutor cannot set the tutor
        Given a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Pablo Ruiz" with "classrooms.update" scope
        When I set teacher "Marta Ibáñez" as the classroom's tutor
        Then I receive a forbidden error

    Scenario: A parent cannot add a teacher to a classroom
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And I am authenticated as "esteban.cristobal@example.com" with "classrooms.update" scope
        When I add teacher "Marta Ibáñez" to the classroom
        Then I receive a forbidden error
