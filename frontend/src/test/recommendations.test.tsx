import { render, screen } from "@testing-library/react";
import { RecommendationList } from "../components/RecommendationList";
import { bedTone } from "../lib/labels";

describe("recommendation presentation", () => {
  it("shows explanations and the no-bed summary", () => {
    render(
      <RecommendationList
        result={{
          summary: "No suitable bed is available. 1 beds were excluded by mandatory rules.",
          candidates: [],
          exclusions: [{ bedId: 4, bedCode: "G-04", wardCode: "GEN", reason: "Missing required equipment: ventilator" }]
        }}
      />
    );
    expect(screen.getByText(/No suitable bed is available/)).toBeInTheDocument();
    expect(screen.getByText(/Missing required equipment: ventilator/)).toBeInTheDocument();
  });

  it("colors an available bed differently from an occupied bed", () => {
    expect(bedTone("AVAILABLE")).toContain("emerald");
    expect(bedTone("OCCUPIED")).toContain("rose");
  });
});
