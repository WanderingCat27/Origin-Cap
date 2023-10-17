
package org.notdev.origincap.cap;

import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

public class CapDataStructureTesting {

    private static final String[] LAYER_CHOICES = { "origins:origin", "origin-classes:class", "test:test" };
    private static final String[] ORIGIN_CHOICES = { "origins:avian", "origins:butterfly", "origins:elytrian",
            "origin-classes:merchant", "test:tesy" };
    private static final UUID[] UUIDS = { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID() };

    private static void displayChoices(Object[] choices) {
        System.out.println(Arrays.toString(choices));
    }

    private static void handleInvalidInput() {
        System.out.println("Invalid input");
    }

    public static void main(String[] args) {
        OriginCap cap = new OriginCap(2);
        Scanner scanner = new Scanner(System.in);
        int layerIndex, originIndex;
        int index;

        while (true) {
            System.out.println("(1) add (2) delete (3) print");
            int input = scanner.nextInt();

            switch (input) {
                case 1:
                    displayChoices(LAYER_CHOICES);
                    layerIndex = scanner.nextInt();
                    displayChoices(ORIGIN_CHOICES);
                    originIndex = scanner.nextInt();
                    displayChoices(UUIDS);
                    System.out.println("index");
                    index = scanner.nextInt();
                    if (index < UUIDS.length) {
                        cap.tryAssign(LAYER_CHOICES[layerIndex - 1], ORIGIN_CHOICES[originIndex - 1], UUIDS[index - 1]);
                    } else {
                        handleInvalidInput();
                    }
                    break;
                case 2:
                    displayChoices(UUIDS);
                    System.out.println("index");
                    index = scanner.nextInt();
                    if (index < UUIDS.length) {
                        cap.removePlayerFromList(UUIDS[index - 1]);
                    } else {
                        handleInvalidInput();
                    }
                    break;
                case 3:
                    System.out.println(cap);
                    break;
                default:
                    handleInvalidInput();
                    break;
            }
        }
    }
}
