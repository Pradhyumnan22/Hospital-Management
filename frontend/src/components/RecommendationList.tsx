import type { Recommendation } from "../api/types";

export function RecommendationList({ result }: { result: Recommendation }) {
  return (
    <div className="space-y-4">
      <p className="rounded-lg bg-teal-50 px-4 py-3 text-sm text-teal-900">{result.summary}</p>
      {result.candidates.length === 0 ? (
        <p className="text-sm text-slate-600">No bed passed the mandatory rules. Scoring was not applied to excluded beds.</p>
      ) : (
        <ol className="space-y-3">
          {result.candidates.map((bed, index) => (
            <li key={bed.bedId} className="rounded-xl border border-slate-200 bg-white p-4">
              <div className="flex items-center justify-between gap-3">
                <p className="font-medium">{index + 1}. {bed.wardCode} / {bed.bedCode}</p>
                <span className="rounded-full bg-slate-900 px-2 py-1 text-xs text-white">Score {bed.score}</span>
              </div>
              <ul className="mt-2 list-disc pl-5 text-sm text-slate-600">
                {bed.explanations.map((line) => <li key={line}>{line}</li>)}
              </ul>
            </li>
          ))}
        </ol>
      )}
      {result.exclusions.length > 0 && (
        <div>
          <h3 className="mb-2 text-sm font-semibold text-slate-800">Excluded by mandatory rules</h3>
          <ul className="space-y-1 text-sm text-slate-600">
            {result.exclusions.slice(0, 8).map((item) => (
              <li key={`${item.bedId}-${item.reason}`}>{item.wardCode} {item.bedCode}: {item.reason}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
