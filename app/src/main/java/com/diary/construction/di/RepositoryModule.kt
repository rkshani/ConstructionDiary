package com.diary.construction.di

import com.diary.construction.data.photo.PhotoStorage
import com.diary.construction.data.repository.ContractorRepositoryImpl
import com.diary.construction.data.repository.ExpenseRepositoryImpl
import com.diary.construction.data.repository.LabourRepositoryImpl
import com.diary.construction.data.repository.MaterialRepositoryImpl
import com.diary.construction.data.repository.PhotoRepositoryImpl
import com.diary.construction.data.repository.ProjectRepositoryImpl
import com.diary.construction.data.repository.StageRepositoryImpl
import com.diary.construction.data.repository.SupplierRepositoryImpl
import com.diary.construction.domain.repository.ContractorRepository
import com.diary.construction.domain.repository.ExpenseRepository
import com.diary.construction.domain.repository.FileStore
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.PhotoRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.repository.SupplierRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun project(impl: ProjectRepositoryImpl): ProjectRepository
    @Binds abstract fun stage(impl: StageRepositoryImpl): StageRepository
    @Binds abstract fun material(impl: MaterialRepositoryImpl): MaterialRepository
    @Binds abstract fun supplier(impl: SupplierRepositoryImpl): SupplierRepository
    @Binds abstract fun labour(impl: LabourRepositoryImpl): LabourRepository
    @Binds abstract fun contractor(impl: ContractorRepositoryImpl): ContractorRepository
    @Binds abstract fun expense(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds abstract fun photo(impl: PhotoRepositoryImpl): PhotoRepository
    @Binds abstract fun fileStore(impl: PhotoStorage): FileStore
}
