Feature: Managing a person's roles

    In order to let a person act as a parent, a teacher, or both, independently of how they first registered
    As an admin
    I want to grant and revoke their roles

    Background:
        Given a school named "Gloria Fuertes" exists

    Scenario: An admin makes a person a parent
        Given a person named "Pablo" "Ruiz" exists with e-mail "pablo.ruiz@example.com"
        And I am authenticated as an admin with "parents.create" scope
        When I make "Pablo" "Ruiz" a parent
        Then I receive a confirmation that the role has been successfully granted
        And "Pablo" "Ruiz" holds the parent role

    Scenario: An admin makes a person a teacher
        Given a person named "Marta" "Ibáñez" exists with e-mail "marta.ibanez@example.com"
        And I am authenticated as an admin with "teachers.create" scope
        When I make "Marta" "Ibáñez" a teacher at school "Gloria Fuertes"
        Then I receive a confirmation that the role has been successfully granted
        And "Marta" "Ibáñez" holds the teacher role

    Scenario: Cannot make a person a teacher without a school
        Given a person named "Marta" "Ibáñez" exists with e-mail "marta.ibanez@example.com"
        And I am authenticated as an admin with "teachers.create" scope
        When I try to make "Marta" "Ibáñez" a teacher without a school
        Then I receive a bad request error
        And "Marta" "Ibáñez" does not hold the teacher role

    Scenario: A person can become both a parent and a teacher
        Given a person named "José" "García" exists with e-mail "jose.garcia@example.com"
        And I am authenticated as an admin with "parents.create" scope
        When I make "José" "García" a parent
        Then "José" "García" holds the parent role
        And I am authenticated as an admin with "teachers.create" scope
        When I make "José" "García" a teacher at school "Gloria Fuertes"
        Then "José" "García" holds the teacher role
        And "José" "García" holds the parent role

    Scenario: A non-admin cannot make a person a parent
        Given a person named "Pablo" "Ruiz" exists with e-mail "pablo.ruiz@example.com"
        And I am authenticated with "parents.create" scope
        When I make "Pablo" "Ruiz" a parent
        Then I receive a forbidden error
        And "Pablo" "Ruiz" does not hold the parent role

    Scenario: A non-admin cannot make a person a teacher
        Given a person named "Marta" "Ibáñez" exists with e-mail "marta.ibanez@example.com"
        And I am authenticated with "teachers.create" scope
        When I make "Marta" "Ibáñez" a teacher at school "Gloria Fuertes"
        Then I receive a forbidden error
        And "Marta" "Ibáñez" does not hold the teacher role

    Scenario: An admin revokes a person's parent role
        Given a person named "Pablo" "Ruiz" exists with e-mail "pablo.ruiz@example.com"
        And "Pablo" "Ruiz" has been made a parent
        And I am authenticated as an admin with "parents.create" scope
        When I revoke "Pablo" "Ruiz"'s parent role
        Then I receive a confirmation that the role has been successfully revoked
        And "Pablo" "Ruiz" does not hold the parent role

    Scenario: A non-admin cannot revoke a person's parent role
        Given a person named "Pablo" "Ruiz" exists with e-mail "pablo.ruiz@example.com"
        And "Pablo" "Ruiz" has been made a parent
        And I am authenticated with "parents.create" scope
        When I revoke "Pablo" "Ruiz"'s parent role
        Then I receive a forbidden error
        And "Pablo" "Ruiz" holds the parent role

    Scenario: Cannot revoke a parent role while they still have kids
        Given a person named "Esteban" "Cristóbal" exists with e-mail "esteban.cristobal@example.com"
        And "Esteban" "Cristóbal" has been made a parent
        And "Esteban" "Cristóbal" has a kid named "Alicia" "Cristóbal" born on "2019-12-12"
        And I am authenticated as an admin with "parents.create" scope
        When I revoke "Esteban" "Cristóbal"'s parent role
        Then I receive a conflict error
        And "Esteban" "Cristóbal" holds the parent role

    Scenario: An admin revokes a person's teacher role
        Given a person named "Marta" "Ibáñez" exists with e-mail "marta.ibanez@example.com"
        And "Marta" "Ibáñez" has been made a teacher at school "Gloria Fuertes"
        And I am authenticated as an admin with "teachers.create" scope
        When I revoke "Marta" "Ibáñez"'s teacher role
        Then I receive a confirmation that the role has been successfully revoked
        And "Marta" "Ibáñez" does not hold the teacher role

    Scenario: A non-admin cannot revoke a person's teacher role
        Given a person named "Marta" "Ibáñez" exists with e-mail "marta.ibanez@example.com"
        And "Marta" "Ibáñez" has been made a teacher at school "Gloria Fuertes"
        And I am authenticated with "teachers.create" scope
        When I revoke "Marta" "Ibáñez"'s teacher role
        Then I receive a forbidden error
        And "Marta" "Ibáñez" holds the teacher role

    Scenario: Cannot revoke a teacher role while they still tutor a classroom
        Given a person named "Marta" "Ibáñez" exists with e-mail "marta.ibanez@example.com"
        And "Marta" "Ibáñez" has been made a teacher at school "Gloria Fuertes"
        And "Marta" "Ibáñez" tutors a classroom
        And I am authenticated as an admin with "teachers.create" scope
        When I revoke "Marta" "Ibáñez"'s teacher role
        Then I receive a conflict error
        And "Marta" "Ibáñez" holds the teacher role

    Scenario: Cannot grant a role to a person that does not exist
        Given I am authenticated as an admin with "parents.create" scope
        When I try to make a person that does not exist a parent
        Then I receive a not found error

    Scenario: Cannot revoke a role a person does not hold
        Given a person named "Pablo" "Ruiz" exists with e-mail "pablo.ruiz@example.com"
        And I am authenticated as an admin with "parents.create" scope
        When I revoke "Pablo" "Ruiz"'s parent role
        Then I receive a not found error
