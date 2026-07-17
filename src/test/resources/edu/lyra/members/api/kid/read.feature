Feature: Kids directory

    In order to look up registered kids
    As a parent or teacher
    I want to list and retrieve the kid records I am entitled to see

    Background:
        Given I am authenticated with "classrooms.read" scope
        And a school named "Gloria Fuertes" exists
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
            | Bruno  | Cristóbal | 2018-05-20 | Esteban Cristóbal |
            | Carla  | Cristóbal | 2020-03-08 | Esteban Cristóbal |
            | Dario  | Cristóbal | 2017-09-15 | Esteban Cristóbal |
            | Elena  | Cristóbal | 2021-01-30 | Esteban Cristóbal |
            | Fabio  | Ibáñez    | 2019-06-01 | Marta Ibáñez      |
            | Gala   | Ibáñez    | 2020-07-11 | Marta Ibáñez      |
        And kid "Alicia" "Cristóbal" is enrolled in classroom for course 1 group "A"
        And kid "Bruno" "Cristóbal" is enrolled in classroom for course 1 group "A"

    Scenario: List kids
        Given I am authenticated as "esteban.cristobal@example.com" with "kids.read" scope
        When I request the list of kids
        Then the list of kids contains exactly the following kids:
            | name   | surname   | birthdate  |
            | Alicia | Cristóbal | 2019-12-12 |
            | Bruno  | Cristóbal | 2018-05-20 |
            | Carla  | Cristóbal | 2020-03-08 |
            | Dario  | Cristóbal | 2017-09-15 |
            | Elena  | Cristóbal | 2021-01-30 |

    Scenario: List kids with pagination
        Given I am authenticated as "esteban.cristobal@example.com" with "kids.read" scope
        When I request the list of kids with page size 2
        Then page 0 contains exactly the following kids:
            | name   | surname   | birthdate  |
            | Alicia | Cristóbal | 2019-12-12 |
            | Bruno  | Cristóbal | 2018-05-20 |
        And page 1 contains exactly the following kids:
            | name  | surname   | birthdate  |
            | Carla | Cristóbal | 2020-03-08 |
            | Dario | Cristóbal | 2017-09-15 |
        And page 2 contains exactly the following kids:
            | name  | surname   | birthdate  |
            | Elena | Cristóbal | 2021-01-30 |

    Scenario: A parent only sees their own kids
        Given I am authenticated as "marta.ibanez@example.com" with "kids.read" scope
        When I request the list of kids
        Then the list of kids contains exactly the following kids:
            | name  | surname | birthdate  |
            | Fabio | Ibáñez  | 2019-06-01 |
            | Gala  | Ibáñez  | 2020-07-11 |

    Scenario: A classroom's tutor sees the kids in that classroom
        Given I am authenticated as teacher "Pablo Ruiz" with "kids.read" scope
        When I request the list of kids
        Then the list of kids contains exactly the following kids:
            | name   | surname   | birthdate  |
            | Alicia | Cristóbal | 2019-12-12 |
            | Bruno  | Cristóbal | 2018-05-20 |

    Scenario: A teacher who is not the tutor also sees the kids in a classroom they teach at
        Given I am authenticated as teacher "José García" with "kids.read" scope
        When I request the list of kids
        Then the list of kids contains exactly the following kids:
            | name   | surname   | birthdate  |
            | Alicia | Cristóbal | 2019-12-12 |
            | Bruno  | Cristóbal | 2018-05-20 |

    Scenario: A user who is neither a parent nor a teacher sees no kids
        Given I am authenticated with "kids.read" scope
        When I request the list of kids
        Then the list of kids contains exactly the following kids:
            | name | surname | birthdate |

    Scenario: An admin sees every kid
        Given I am authenticated as an admin with "kids.read" scope
        When I request the list of kids
        Then the list of kids contains exactly the following kids:
            | name   | surname   | birthdate  |
            | Alicia | Cristóbal | 2019-12-12 |
            | Bruno  | Cristóbal | 2018-05-20 |
            | Carla  | Cristóbal | 2020-03-08 |
            | Dario  | Cristóbal | 2017-09-15 |
            | Elena  | Cristóbal | 2021-01-30 |
            | Fabio  | Ibáñez    | 2019-06-01 |
            | Gala   | Ibáñez    | 2020-07-11 |

    Scenario: Get a kid
        Given I am authenticated with "kids.read" scope
        When I request kid "Alicia" "Cristóbal"
        Then I receive the details of kid "Alicia" "Cristóbal"

    Scenario: Cannot get a kid that does not exist
        Given I am authenticated with "kids.read" scope
        When I request a kid that does not exist
        Then I receive a not found error
