Feature: Binding a kid to a parent

    In order to attach an existing kid record to the right parent account
    As a parent or Lyra administrator
    I want to bind a kid to a parent

    Background:
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
            | Marta   | Ibáñez    | marta.ibanez@example.com      |

    Scenario: An admin can bind any kid to any parent
        Given kid "Alicia" "Cristóbal" was created by parent "Esteban Cristóbal" and is not yet bound to any parent
        And I am authenticated as an admin with "parents.update" scope
        When I bind kid "Alicia" "Cristóbal" to parent "Esteban Cristóbal"
        Then I receive a confirmation that the kid has been successfully bound to the parent

    Scenario: A parent can bind to themselves a kid they created
        Given kid "Alicia" "Cristóbal" was created by parent "Esteban Cristóbal" and is not yet bound to any parent
        And I am authenticated as "esteban.cristobal@example.com" with "parents.update" scope
        When I bind kid "Alicia" "Cristóbal" to parent "Esteban Cristóbal"
        Then I receive a confirmation that the kid has been successfully bound to the parent

    Scenario: A parent cannot bind to themselves a kid created by someone else
        Given kid "Alicia" "Cristóbal" was created by parent "Marta Ibáñez" and is not yet bound to any parent
        And I am authenticated as "esteban.cristobal@example.com" with "parents.update" scope
        When I bind kid "Alicia" "Cristóbal" to parent "Esteban Cristóbal"
        Then I receive a forbidden error

    Scenario: A parent cannot bind a kid they created to a different parent's account
        Given kid "Alicia" "Cristóbal" was created by parent "Esteban Cristóbal" and is not yet bound to any parent
        And I am authenticated as "esteban.cristobal@example.com" with "parents.update" scope
        When I bind kid "Alicia" "Cristóbal" to parent "Marta Ibáñez"
        Then I receive a forbidden error

    Scenario: A teacher cannot bind a kid to a parent
        Given kid "Alicia" "Cristóbal" was created by parent "Esteban Cristóbal" and is not yet bound to any parent
        And a school named "Gloria Fuertes" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Pablo Ruiz" with "parents.update" scope
        When I bind kid "Alicia" "Cristóbal" to parent "Esteban Cristóbal"
        Then I receive a forbidden error
