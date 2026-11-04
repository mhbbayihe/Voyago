package cm.enspd.voyago.trip.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Plan des sièges découpé en rangées, avec l'allée après {@code aisleAfter} sièges. */
public record SeatMapView(int seatCount, int seatsPerRow, int aisleAfter, List<List<Integer>> rows, Set<Integer> occupied) {

	public static SeatMapView build(int seatCount, int seatsPerRow, Set<Integer> occupied) {
		List<List<Integer>> rows = new ArrayList<>();
		for (int start = 1; start <= seatCount; start += seatsPerRow) {
			List<Integer> row = new ArrayList<>();
			for (int seat = start; seat < start + seatsPerRow && seat <= seatCount; seat++) {
				row.add(seat);
			}
			rows.add(row);
		}
		return new SeatMapView(seatCount, seatsPerRow, (seatsPerRow + 1) / 2, rows, occupied);
	}

	public boolean isOccupied(int seat) {
		return occupied.contains(seat);
	}
}
