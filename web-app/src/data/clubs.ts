export interface CreateClubInput {
  name: string;
  shortName: string;
}

export interface Club {
  id: number;
  name: string;
  shortName: string;
  createdAt: string;
}
