export type Role = "ADMIN" | "ADMISSION_STAFF" | "CLINICAL_STAFF";
export type Sex = "FEMALE" | "MALE" | "OTHER" | "UNKNOWN";
export type WardType = "GENERAL" | "ICU" | "ISOLATION" | "PEDIATRIC" | "MATERNITY";
export type BedState = "AVAILABLE" | "OCCUPIED" | "CLEANING" | "OUT_OF_SERVICE";
export type AdmissionStatus = "ACTIVE" | "DISCHARGED";

export interface User {
  id: number;
  username: string;
  fullName: string;
  role: Role;
  enabled: boolean;
}

export interface Patient {
  id: number;
  medicalRecordNumber: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  sex: Sex;
  phone: string | null;
  createdAt: string;
}

export interface Ward {
  id: number;
  code: string;
  name: string;
  wardType: WardType;
  floorNumber: number;
}

export interface Bed {
  id: number;
  wardId: number;
  wardCode: string;
  wardName: string;
  wardType: WardType;
  bedCode: string;
  state: BedState;
  isolationCapable: boolean;
  ventilator: boolean;
  monitor: boolean;
  oxygen: boolean;
}

export interface Admission {
  id: number;
  patientId: number;
  patientName: string;
  medicalRecordNumber: string;
  bedId: number;
  bedCode: string;
  wardCode: string;
  wardName: string;
  status: AdmissionStatus;
  diagnosis: string;
  isolationRequired: boolean;
  requiredWardType: WardType | null;
  admittedAt: string;
  dischargedAt: string | null;
  admittedBy: string;
  dischargedBy: string | null;
  dischargeNotes: string | null;
}

export interface WardOccupancy {
  wardCode: string;
  wardName: string;
  operationalBeds: number;
  occupiedBeds: number;
  availableBeds: number;
  occupancyPercent: number;
}

export interface Overview {
  operationalBeds: number;
  occupiedBeds: number;
  availableBeds: number;
  cleaningBeds: number;
  outOfServiceBeds: number;
  occupancyPercent: number;
  wards: WardOccupancy[];
  measured: boolean;
}

export interface DailyFlow {
  date: string;
  admissions: number;
  discharges: number;
}

export interface Forecast {
  method: string;
  averageDailyNetChange: number;
  lowConfidence: boolean;
  limitations: string;
  next24Hours: { hourOffset: number; predictedOccupied: number; predictedOccupancyPercent: number; predicted: boolean }[];
  next7Days: { date: string; predictedOccupied: number; predictedOccupancyPercent: number; predicted: boolean }[];
  history: DailyFlow[];
}

export interface RankedBed {
  bedId: number;
  bedCode: string;
  wardCode: string;
  wardName: string;
  wardType: WardType;
  score: number;
  explanations: string[];
}

export interface Recommendation {
  candidates: RankedBed[];
  exclusions: { bedId: number; bedCode: string; wardCode: string; reason: string }[];
  summary: string;
}

export interface Notice {
  id: number;
  severity: string;
  title: string;
  message: string;
  acknowledged: boolean;
  createdAt: string;
}

export interface Criteria {
  patientId: number;
  diagnosis: string;
  requiredWardType: WardType | "";
  preferredWardType: WardType | "";
  isolationRequired: boolean;
  ventilatorRequired: boolean;
  monitorRequired: boolean;
  oxygenRequired: boolean;
}
