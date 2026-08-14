import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { makeCatalogEntry } from "../test/fixtures";
import { BuildingDisplay } from "./BuildingDisplay";

describe("BuildingDisplay", () => {
  const catalog = [
    makeCatalogEntry("SMALL_MARKET", { cost: 1, victoryPoints: 1, copies: 2 }),
    makeCatalogEntry("HOSPICE", { cost: 4, victoryPoints: 2, copies: 2 }),
    makeCatalogEntry("HARBOR", { cost: 8, victoryPoints: 3, copies: 2 }),
    makeCatalogEntry("CITY_HALL", { cost: 10, victoryPoints: 4, copies: 1 }),
  ];

  it("sorts each building into the column its victory point value gives it", () => {
    render(<BuildingDisplay catalog={catalog} supply={{}} />);

    expect(
      within(screen.getByTestId("building-display-column-1")).getByTestId(
        "building-card-SMALL_MARKET",
      ),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("building-display-column-4")).getByTestId(
        "building-card-CITY_HALL",
      ),
    ).toBeInTheDocument();
  });

  it("shows the printed cost and what each building does", () => {
    render(<BuildingDisplay catalog={catalog} supply={{ HOSPICE: 2 }} />);

    expect(screen.getByTestId("building-cost-HOSPICE")).toHaveTextContent("4");
    expect(screen.getByTestId("building-card-HOSPICE")).toHaveTextContent(
      "a colonist arrives on each plantation you settle",
    );
  });

  it("counts what is left against what the game supplies", () => {
    render(<BuildingDisplay catalog={catalog} supply={{ SMALL_MARKET: 1, HOSPICE: 2 }} />);

    expect(screen.getByTestId("building-supply-SMALL_MARKET")).toHaveTextContent("1 of 2 left");
    expect(screen.getByTestId("building-supply-HOSPICE")).toHaveTextContent("2 of 2 left");
  });

  /** A type missing from the supply map is sold out, not unknown — the wire omits nothing else. */
  it("greys out a sold-out building rather than dropping it", () => {
    render(<BuildingDisplay catalog={catalog} supply={{ SMALL_MARKET: 2 }} />);

    const soldOut = screen.getByTestId("building-supply-CITY_HALL");
    expect(soldOut).toHaveAttribute("data-sold-out", "true");
    expect(soldOut).toHaveTextContent("0 of 1 left");
    expect(screen.getByTestId("building-supply-SMALL_MARKET")).toHaveAttribute(
      "data-sold-out",
      "false",
    );
  });
});
