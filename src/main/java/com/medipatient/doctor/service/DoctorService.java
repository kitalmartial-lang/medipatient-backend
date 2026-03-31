package com.medipatient.doctor.service;

import com.medipatient.doctor.dto.CreateDoctorDto;
import com.medipatient.doctor.dto.DoctorDto;
import com.medipatient.doctor.dto.UpdateDoctorDto;
import com.medipatient.doctor.mapper.DoctorMapper;
import com.medipatient.doctor.model.Doctor;
import com.medipatient.doctor.repository.DoctorRepository;
import com.medipatient.profile.model.Profile;
import com.medipatient.profile.repository.ProfileRepository;
import com.medipatient.specialty.model.Specialty;
import com.medipatient.specialty.repository.SpecialtyRepository;
import com.medipatient.doctor.model.AvailabilityStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ProfileRepository profileRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorMapper doctorMapper;

    @Transactional(readOnly = true)
    public Page<DoctorDto> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DoctorDto> searchDoctors(String search, UUID specialtyId, 
                                        AvailabilityStatus availabilityStatus,
                                        Pageable pageable) {
        return doctorRepository.searchDoctors(search, specialtyId, availabilityStatus, pageable)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DoctorDto> getDoctorsBySpecialty(UUID specialtyId, Pageable pageable) {
        return doctorRepository.findBySpecialtyId(specialtyId, pageable)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DoctorDto> getDoctorsByAvailabilityStatus(AvailabilityStatus status, Pageable pageable) {
        return doctorRepository.findByAvailabilityStatus(status, pageable)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DoctorDto> getDoctorsByConsultationFeeRange(Integer minFee, Integer maxFee, Pageable pageable) {
        return doctorRepository.findByConsultationFeeBetween(minFee, maxFee, pageable)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<DoctorDto> getDoctorById(UUID id) {
        return doctorRepository.findById(id)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<DoctorDto> getDoctorByUserId(UUID userId) {
        return doctorRepository.findByUserId(userId)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<DoctorDto> getDoctorByLicenseNumber(String licenseNumber) {
        return doctorRepository.findByLicenseNumber(licenseNumber)
                .map(doctorMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<DoctorDto> getAvailableDoctorsForDate(LocalDate date) {
        return doctorRepository.findAvailableDoctorsForDate(date)
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    public DoctorDto createDoctor(CreateDoctorDto createDoctorDto) {
        // 1. Récupération du profil
        Profile user = profileRepository.findById(createDoctorDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found with id: " + createDoctorDto.getUserId()));

        // 2. Vérifications de sécurité
        if (!user.getRole().equals(Profile.Role.DOCTOR)) {
            throw new IllegalArgumentException("Profile must have DOCTOR role to create doctor record");
        }
        if (doctorRepository.findByUserId(createDoctorDto.getUserId()).isPresent()) {
            throw new IllegalArgumentException("Doctor record already exists for this user");
        }

        // 3. CONSTRUCTION MANUELLE (On n'utilise plus doctorMapper.toEntity ici)
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setLicenseNumber(createDoctorDto.getLicenseNumber());
        doctor.setConsultationFee(createDoctorDto.getConsultationFee());
        doctor.setAvailabilityStatus(createDoctorDto.getAvailabilityStatus()); // Utilise le bon Enum

        // On initialise les dates pour éviter les NullPointer
        doctor.setCreatedAt(java.time.ZonedDateTime.now());
        doctor.setUpdatedAt(java.time.ZonedDateTime.now());

        // 4. Gestion de la spécialité
        if (createDoctorDto.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(createDoctorDto.getSpecialtyId())
                    .orElseThrow(() -> new IllegalArgumentException("Specialty not found with id: " + createDoctorDto.getSpecialtyId()));
            doctor.setSpecialty(specialty);
        }

        // 5. Sauvegarde
        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Created new doctor with id: {}", savedDoctor.getId());

        // On utilise le mapper uniquement pour le retour (vers DTO), ce qui pose moins de soucis
        return doctorMapper.toDto(savedDoctor);
    }
    public DoctorDto updateDoctor(UUID id, UpdateDoctorDto updateDoctorDto) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + id));

        if (updateDoctorDto.getLicenseNumber() != null &&
            !updateDoctorDto.getLicenseNumber().equals(doctor.getLicenseNumber()) &&
            doctorRepository.existsByLicenseNumber(updateDoctorDto.getLicenseNumber())) {
            throw new IllegalArgumentException("License number already exists: " + updateDoctorDto.getLicenseNumber());
        }

        doctorMapper.updateEntityFromDto(updateDoctorDto, doctor);

        if (updateDoctorDto.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(updateDoctorDto.getSpecialtyId())
                    .orElseThrow(() -> new IllegalArgumentException("Specialty not found with id: " + updateDoctorDto.getSpecialtyId()));
            doctor.setSpecialty(specialty);
        }

        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Updated doctor with id: {}", savedDoctor.getId());

        return doctorMapper.toDto(savedDoctor);
    }

    public void deleteDoctor(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + id));

        doctorRepository.delete(doctor);
        log.info("Deleted doctor with id: {}", id);
    }

    public DoctorDto updateAvailabilityStatus(UUID id, AvailabilityStatus status) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + id));

        doctor.setAvailabilityStatus(status);
        Doctor savedDoctor = doctorRepository.save(doctor);
        
        log.info("Updated availability status for doctor {} to {}", id, status);
        
        return doctorMapper.toDto(savedDoctor);
    }

    @Transactional(readOnly = true)
    public boolean existsByLicenseNumber(String licenseNumber) {
        return doctorRepository.existsByLicenseNumber(licenseNumber);
    }

    @Transactional(readOnly = true)
    public long countByAvailabilityStatus(AvailabilityStatus status) {
        return doctorRepository.countByAvailabilityStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSpecialtyStatistics() {
        return doctorRepository.countBySpecialty();
    }
}