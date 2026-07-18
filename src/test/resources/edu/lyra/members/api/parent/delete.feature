Feature: Parent account deletion

    In order to keep parent accounts accurate
    As a parent or Lyra administrator
    I want to delete a parent's account

    Background:
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
            | Marta   | Ibáñez    | marta.ibanez@example.com      |

    Scenario: An admin can delete a childless parent
        Given I am authenticated as an admin with "parents.delete" scope
        When I delete parent "Esteban" "Cristóbal"
        Then I receive a confirmation that the parent account has been successfully deleted

    Scenario: A parent can delete their own childless account
        Given I am authenticated as "esteban.cristobal@example.com" with "parents.delete" scope
        When I delete parent "Esteban" "Cristóbal"
        Then I receive a confirmation that the parent account has been successfully deleted

    Scenario: A parent cannot delete another parent's account
        Given I am authenticated as "marta.ibanez@example.com" with "parents.delete" scope
        When I delete parent "Esteban" "Cristóbal"
        Then I receive a forbidden error

    Scenario: A teacher cannot delete a parent's account
        Given a school named "Gloria Fuertes" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Pablo Ruiz" with "parents.delete" scope
        When I delete parent "Esteban" "Cristóbal"
        Then I receive a forbidden error

    Scenario: Cannot delete a parent that does not exist
        Given I am authenticated as an admin with "parents.delete" scope
        When I delete a parent that does not exist
        Then I receive a not found error

    Scenario: An admin cannot delete a parent that still has kids
        Given the following kids exist:
            | name   | surname   | birthdate  | parent            |
            | Alicia | Cristóbal | 2019-12-12 | Esteban Cristóbal |
        And I am authenticated as an admin with "parents.delete" scope
        When I delete parent "Esteban" "Cristóbal"
        Then I receive a conflict error

    Scenario: A parent cannot delete their own account while they still have kids
        Given the following kids exist:
            | name   | surname   | birthdate  | parent            |
            | Alicia | Cristóbal | 2019-12-12 | Esteban Cristóbal |
        And I am authenticated as "esteban.cristobal@example.com" with "parents.delete" scope
        When I delete parent "Esteban" "Cristóbal"
        Then I receive a conflict error
